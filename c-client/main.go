/*

Copyright 2020 Q-Jam B.V.

*/
package main

import (
	"encoding/hex"
	"encoding/json"
	"fmt"
	"math/rand"
	"os"
	"path/filepath"

	"github.com/sirupsen/logrus"

	"q-jam.nl/c/c-client/package_manager"
)

var Log = logrus.New()

type Report struct {
	Hostname string                    `json:"h"`
	Packages []package_manager.Package `json:"p"`
	Docker   []DockerContainer         `json:"d"`
}

func main() {
	Log.Level = logrus.DebugLevel

	var packageManagers = []package_manager.PackageManager{
		package_manager.ApkPackageManagerImpl{},
		package_manager.DebPackageManagerImpl{},
	}

	// Figure out system wide packages
	reportPackages, err := getPackages(packageManagers)
	if err != nil {
		panic(err)
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

	hostname, err := os.Hostname()
	if err != nil {
		panic(err)
	}

	// The final report to send
	report := Report{
		Hostname: hostname,
		Packages: reportPackages,
		Docker:   reportDockerContainers,
	}

	reportAsJson, err := json.Marshal(report)
	fmt.Println(string(reportAsJson))
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
