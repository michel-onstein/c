/*

Copyright 2020 Q-Jam B.V.

*/
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"os/user"
)

func LoginCmd(username string, password string) (string, error) {
	// Figure out how to login to begin with
	loginInfoRequestAsJson, err := json.Marshal(LoginInfoRequest{
		Username: username,
	})
	if err != nil {
		panic(err)
	}

	request, err := http.NewRequest("POST", ConfigurationInstance.ApiEndpoint+"/api/v1/login-info",
		bytes.NewBuffer(loginInfoRequestAsJson))
	request.Header.Set("Content-Type", "application/json")

	client := &http.Client{}
	response, err := client.Do(request)
	if err != nil {
		return "", fmt.Errorf("error getting login-info: %s", err)
	}

	defer response.Body.Close()

	if response.StatusCode != 200 {
		return "", fmt.Errorf("username and/or password incorrect")
	}

	body, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return "", fmt.Errorf("error reading login-info response: %s", err)
	}

	loginInfoResponse := LoginInfoResponse{}
	err = json.Unmarshal(body, &loginInfoResponse)
	if err != nil {
		return "", fmt.Errorf("error unmarshalling login-info response: %s", err)
	}

	// Check sanity, we only support plain password authentication right now
	if loginInfoResponse.Type != "p" {
		return "", fmt.Errorf("unsupported login type '%s'", loginInfoResponse.Type)
	}

	//
	// Plain username password login
	//
	plainLoginRequest, err := json.Marshal(PlainLoginRequest{
		Username: username,
		Password: password,
	})
	if err != nil {
		panic(err)
	}

	request, err = http.NewRequest("POST", ConfigurationInstance.ApiEndpoint+"/api/v1/plain-login",
		bytes.NewBuffer(plainLoginRequest))
	request.Header.Set("Content-Type", "application/json")

	response, err = client.Do(request)
	if err != nil {
		return "", fmt.Errorf("error performing plain login: %s", err)
	}

	defer response.Body.Close()

	if response.StatusCode != 200 {
		return "", fmt.Errorf("username and/or password incorrect")
	}

	body, err = ioutil.ReadAll(response.Body)
	if err != nil {
		return "", fmt.Errorf("error reading login response response: %s", err)
	}

	loginResponse := LoginResponse{}
	err = json.Unmarshal(body, &loginResponse)
	if err != nil {
		return "", fmt.Errorf("error unmarshalling login response: %s", err)
	}

	return loginResponse.Token, nil
}

// Logout by simply removing the configuration file, may not be the best later
func LogoutCmd() {
	currentUser, err := user.Current()
	if err != nil {
		panic(err)
	}

	var filename = currentUser.HomeDir + "/." + CONFIGURATION_FILENAME
	os.Remove(filename)

	return
}

type LoginInfoRequest struct {
	Username string `json:"username"`
}

type LoginInfoResponse struct {
	Type string `json:"type"`
}

type PlainLoginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type LoginResponse struct {
	Token string `json:"token"`

	// Username ignored for now
}
