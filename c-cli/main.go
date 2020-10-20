/*

Copyright 2020 Zededa Inc.

*/
package main

import (
	"encoding/json"
	"fmt"
	"github.com/alecthomas/kong"
	"io/ioutil"
	"os"
	"os/user"
)

var CLI struct {
	Login struct {
		Username    string `short:"u" help:"the username"`
		Password    string `short:"p" help:"the password"`
		ApiEndpoint string `help:"the API endpoint URL"`
	} `cmd help:"Login to the API."`

	Logout struct {
	} `cmd help:"Logout."`

	Rm struct {
		Force     bool `help:"Force removal."`
		Recursive bool `help:"Recursively remove files."`

		Paths []string `arg name:"path" help:"Paths to remove." type:"path"`
	} `cmd help:"Remove files."`

	Ls struct {
		Paths []string `arg optional name:"path" help:"Paths to list." type:"path"`
	} `cmd help:"List paths."`

	OutputType string `help:"output type, text or json"`
}

type Configuration struct {
	Username    string `json:"username"`
	Token       string `json:"token"`
	ApiEndpoint string `json:"api_endpoint"`
}

var ConfigurationInstance *Configuration = &Configuration{
	ApiEndpoint: DEFAULT_API_ENDPOINT,
}

const CONFIGURATION_FILENAME = "c-cli"
const DEFAULT_API_ENDPOINT = "http://localhost:1080"

func main() {
	//switch ctx.Command() {
	//case "rm <path>":
	//case "ls":
	//default:
	//	panic(ctx.Command())
	//}

	// Restore configuration if we have any
	err := restoreConfiguration()
	if err != nil {
		panic(err)
	}

	ctx := kong.Parse(&CLI)
	switch ctx.Command() {
	case "login":
		// TODO Allow Console input
		token, err := Login(CLI.Login.Username, CLI.Login.Password)
		if err != nil {
			fmt.Printf("login: %s\n", err)
			os.Exit(-1)
		}

		ConfigurationInstance.Token = token
		ConfigurationInstance.Username = CLI.Login.Username

		err = storeConfiguration()
		if err != nil {
			fmt.Printf("error storing configuration: %s", err)
			os.Exit(-2)
		}
		return
	case "logout":
		Logout()
		return
	}

	// Something other than login, logout, requires actually being logged in first
	if ConfigurationInstance.Username == "" {
		fmt.Println("Please login first.")
		return
	}
}

// Restore configuration from disk if any
func restoreConfiguration() error {
	currentUser, err := user.Current()
	if err != nil {
		return fmt.Errorf("error getting current user: %s", err)
	}

	var filename = currentUser.HomeDir + "/." + CONFIGURATION_FILENAME
	info, err := os.Stat(filename)

	if os.IsNotExist(err) {
		return nil
	}

	if info.IsDir() {
		return fmt.Errorf("configuration file is a directory")
	}

	marshalled, err := ioutil.ReadFile(filename)
	if err != nil {
		return fmt.Errorf("error reading configuration file: %s", err)
	}

	err = json.Unmarshal(marshalled, ConfigurationInstance)
	if err != nil {
		return fmt.Errorf("error unmarshalling configuration: %s", err)
	}

	return nil
}

// Store configuration to disk, e.g. after login
func storeConfiguration() error {
	currentUser, err := user.Current()
	if err != nil {
		return fmt.Errorf("error getting current user: %s", err)
	}

	var filename = currentUser.HomeDir + "/." + CONFIGURATION_FILENAME

	marshalled, err := json.Marshal(ConfigurationInstance)
	if err != nil {
		panic(err)
	}

	err = ioutil.WriteFile(filename, marshalled, 0600)
	if err != nil {
		return fmt.Errorf("error writing configuration file: %s", err)
	}

	return nil
}
