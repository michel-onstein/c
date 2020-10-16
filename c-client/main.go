/*

Copyright 2020 Q-Jam B.V.

*/
package main

import (
	"bytes"
	"encoding/hex"
	"encoding/json"
	"flag"
	"fmt"
	"math/rand"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"q-jam.nl/c/c-client/package_manager"
)

var Log = logrus.New()

type Report struct {
	UUID     string                    `json:"u"`
	Hostname string                    `json:"h"`
	Time     int64                     `json:"t"`
	Packages []package_manager.Package `json:"p"`
	Docker   []DockerContainer         `json:"d"`
}

type Configuration struct {
	APIEndpoint string
	APIKey      string
	LogLevel    logrus.Level
}

func main() {
	configuration, err := getConfiguration()
	if err != nil {
		fmt.Printf("configuration error: %s", err)
		os.Exit(-1)
	}

	configurationAsJson, err := json.Marshal(configuration)
	fmt.Println(string(configurationAsJson))

	Log.Level = logrus.DebugLevel

	var packageManagers = []package_manager.PackageManager{
		package_manager.ApkPackageManagerImpl{},
		package_manager.DebPackageManagerImpl{},
	}

	report, err := report(packageManagers)
	if err != nil {
		fmt.Printf("error generating report: %s", err)
		os.Exit(-2)
	}

	reportAsJson, err := json.Marshal(report)
	if err != nil {
		fmt.Printf("error marshalling report: %s", err)
		os.Exit(-3)
	}

	// Send report to server
	client := http.Client{}
	request, err := http.NewRequest("POST", configuration.APIEndpoint+"/report", bytes.NewBuffer(reportAsJson))
	if err != nil {
		panic(err)
	}
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("X-API-KEY", configuration.APIKey)

	response, err := client.Do(request)
	if err != nil {
		fmt.Printf("error executing http request: %s", err)
		os.Exit(-4)
	}

	defer response.Body.Close()

	//fmt.Println(string(reportAsJson))
}

// Get the configuration from the command line and environment
func getConfiguration() (Configuration, error) {
	// Parse commandline
	apiEndpointPtr := flag.String("api-endpoint", "http://localhost:1080/api/v1", "The API endpoint URL")
	apiKeyPtr := flag.String("api-key", "", "The API key")
	logLevelAsStringPtr := flag.String("log-level", "info", "Log level")

	flag.Parse()

	// Parse environment
	if os.Getenv("API_ENDPOINT") != "" {
		// TODO Does a construct like this cause issues with the GC
		apiEndpoint := os.Getenv("API_ENDPOINT")
		apiEndpointPtr = &apiEndpoint
	}

	// Parse log level
	var logLevel logrus.Level
	logLevelAsString := strings.ToLower(*logLevelAsStringPtr)
	switch logLevelAsString {
	case "trace":
		logLevel = logrus.TraceLevel
		break
	case "debug":
		logLevel = logrus.DebugLevel
		break
	case "info":
		logLevel = logrus.InfoLevel
		break
	case "warning":
		logLevel = logrus.WarnLevel
		break
	case "error":
		logLevel = logrus.ErrorLevel
		break
	default:
		return Configuration{}, fmt.Errorf("unknown log-level \"%s\"", logLevelAsString)
	}

	// Validation
	if *apiKeyPtr == "" {
		return Configuration{}, fmt.Errorf("api-key not specified")
	}

	configuration := Configuration{
		APIEndpoint: *apiEndpointPtr,
		APIKey:      *apiKeyPtr,
		LogLevel:    logLevel,
	}

	return configuration, nil
}

func report(packageManagers []package_manager.PackageManager) (*Report, error) {
	// Figure out system wide packages
	reportPackages, err := getPackages(packageManagers)
	if err != nil {
		return nil, err
	}

	// Figure out the packages in the docker containers
	var reportDockerContainers []DockerContainer
	dockerPackages, err := GetDockerPackages(packageManagers)
	if err != nil {
		Log.Debugf("getting docker container packages failed, likely simply no docker: %v", err)
	} else {

		for _, dockerContainer := range dockerPackages {
			reportDockerContainers = append(reportDockerContainers, dockerContainer)
		}
	}

	// Get the hostname
	hostname, err := os.Hostname()
	if err != nil {
		return nil, err
	}

	// Generate a UUID for the report
	rand.Seed(time.Now().UnixNano())
	uuidAsBytes := make([]byte, 32)
	_, err = rand.Read(uuidAsBytes)
	if err != nil {
		return nil, err
	}
	uuid := hex.EncodeToString(uuidAsBytes)

	// The final report to send
	report := Report{
		UUID:     uuid,
		Hostname: hostname,
		Time:     time.Now().Unix(),
		Packages: reportPackages,
		Docker:   reportDockerContainers,
	}

	return &report, nil
}

// Get system package for provided package managers
func getPackages(packageManagers []package_manager.PackageManager) ([]package_manager.Package, error) {
	var allPackages []package_manager.Package

	for _, packageManager := range packageManagers {
		var files = packageManager.FilesNeeded()

		// Figure out if all files required by the package manager exist
		var allFilesPresent = true
		for _, file := range files {
			info, err := os.Stat(file)
			if os.IsNotExist(err) {
				allFilesPresent = false
			} else {
				if info.IsDir() {
					allFilesPresent = false
				}
			}
		}

		if allFilesPresent {
			Log.Debugf("found package manager: %s\n", packageManager.Id())

			packages := packageManager.Get(files)

			allPackages = append(allPackages, packages...)

			if Log.IsLevelEnabled(logrus.TraceLevel) {
				for _, pkg := range packages {
					Log.Tracef("Package: %s, Version: %s (%s)\n", pkg.Name, pkg.Version, pkg.Manager)
				}
			}
		}
	}

	return allPackages, nil
}

// Generate a random temporary filename
func TempFileName(prefix string) string {
	randBytes := make([]byte, 16)
	rand.Read(randBytes)
	return filepath.Join(os.TempDir(), prefix+hex.EncodeToString(randBytes))
}
