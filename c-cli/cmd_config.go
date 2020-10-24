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
	"net/url"
)

func ConfigLsCmd(global bool, outputType string) error {

	request, err := http.NewRequest("GET", ConfigurationInstance.ApiEndpoint+"/api/v1/configuration", nil)
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("Authorization", "Bearer "+ConfigurationInstance.Token)

	client := &http.Client{}
	response, err := client.Do(request)
	if err != nil {
		return fmt.Errorf("error getting configuration: %s", err)
	}

	defer response.Body.Close()

	if response.StatusCode != 200 {
		return StatusCodeToError(response.StatusCode)
	}

	body, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return fmt.Errorf("error reading configuration response: %s", err)
	}

	unmarshalled := make([]ConfigurationEntry, 0)
	err = json.Unmarshal(body, &unmarshalled)
	if err != nil {
		return fmt.Errorf("error unmarshalling configuration response: %s", err)
	}

	if CLI.OutputType == "text" {
		return fmt.Errorf("output type 'text' not implemented")
	} else {
		// Make a standard map out of the unmarshalled json
		output := make(map[string]string)

		for _, entry := range unmarshalled {
			output[entry.Key] = entry.Value
		}

		// Output as json
		outputAsJson, err := json.Marshal(output)
		if err != nil {
			panic(err)
		}

		fmt.Println(string(outputAsJson))
	}

	return nil
}

func ConfigSetCmd(key string, value string) error {
	entry := ConfigurationEntry{
		Key:   key,
		Value: value,
	}
	marshalled, err := json.Marshal(entry)
	if err != nil {
		panic(err)
	}

	request, err := http.NewRequest("PUT", ConfigurationInstance.ApiEndpoint+"/api/v1/configuration",
		bytes.NewBuffer(marshalled))
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("Authorization", "Bearer "+ConfigurationInstance.Token)

	client := &http.Client{}
	response, err := client.Do(request)
	if err != nil {
		return fmt.Errorf("error setting configuration: %s", err)
	}

	defer response.Body.Close()

	if response.StatusCode != 200 {
		return StatusCodeToError(response.StatusCode)
	}

	return nil
}

func ConfigRmCmd(key string) error {
	request, err := http.NewRequest("DELETE",
		ConfigurationInstance.ApiEndpoint+"/api/v1/configuration/"+url.QueryEscape(key), nil)
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("Authorization", "Bearer "+ConfigurationInstance.Token)

	client := &http.Client{}
	response, err := client.Do(request)
	if err != nil {
		return fmt.Errorf("error removing configuration: %s", err)
	}

	defer response.Body.Close()

	switch response.StatusCode {
	case 200:
		return nil
	default:
		return StatusCodeToError(response.StatusCode)
	}
}

func ConfigGetCmd(key string) error {
	request, err := http.NewRequest("GET",
		ConfigurationInstance.ApiEndpoint+"/api/v1/configuration/"+url.QueryEscape(key), nil)
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("Authorization", "Bearer "+ConfigurationInstance.Token)

	client := &http.Client{}
	response, err := client.Do(request)
	if err != nil {
		return fmt.Errorf("error getting configuration: %s", err)
	}

	defer response.Body.Close()

	if response.StatusCode != 200 {
		return StatusCodeToError(response.StatusCode)
	}

	body, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return fmt.Errorf("error reading configuration response: %s", err)
	}

	fmt.Println(string(body))

	return nil
}

type ConfigurationEntry struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

func StatusCodeToError(statusCode int) error {
	switch statusCode {
	case 401:
		return fmt.Errorf("unauthorized")
	case 404:
		return fmt.Errorf("not found")
	case 500:
		return fmt.Errorf("internal server error")
	default:
		return fmt.Errorf("TODO: Statuscode: %d", statusCode)
	}
}
