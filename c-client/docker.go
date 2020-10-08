/*

Copyright 2020 Zededa Inc.

*/
package main

import (
	"archive/tar"
	"context"
	"fmt"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/client"
	"io"
	"os"
	"q-jam.nl/c/c-client/package_manager"
)

type DockerContainer struct {
	ID       string                    `json:"i"`
	Image    string                    `json:"m"`
	Packages []package_manager.Package `json:"p"`
}

// Get packages in all docker containers for provided package managers
func GetDockerPackages(packageManagers []package_manager.PackageManager) (map[string]DockerContainer, error) {
	cli, err := client.NewClient("unix:///var/run/docker.sock", "v1.22", nil, nil)
	if err != nil {
		return nil, fmt.Errorf("error getting docker packages: %v", err)
	}

	containers, err := cli.ContainerList(context.Background(), types.ContainerListOptions{})
	if err != nil {
		return nil, fmt.Errorf("error getting docker packages: %v", err)
	}

	result := make(map[string]DockerContainer)

	// Iterate all containers
	for _, container := range containers {
		var allPackages []package_manager.Package

		for _, packageManager := range packageManagers {
			files := packageManager.FilesNeeded()

			// Figure out if all files required by the package manager exist
			allFilesPresent := true
			fileMap := make(map[string]string)
			for _, file := range files {
				temp := TempFileName("df-")

				err = copyFileFromDockerContainer(cli, container, file, temp)
				if err != nil {
					allFilesPresent = false
					break
				}

				fileMap[file] = temp
			}

			if !allFilesPresent {
				continue
			}

			Log.Debugf("Found package manager: %s for docker container %s (%s)", packageManager.Id(), container.ID, container.Image)

			// Construct a slice with the temporary files in the right order
			var tempFiles []string
			for _, file := range files {
				tempFiles = append(tempFiles, fileMap[file])
			}

			// Determine the packages
			packages := packageManager.Get(tempFiles)

			allPackages = append(allPackages, packages...)

			// Delete temporary files
			for _, temp := range fileMap {
				err = os.Remove(temp)
				if err != nil {
					return nil, fmt.Errorf("error getting docker packages: %v", err)
				}
			}
		}

		result[container.ID] = DockerContainer{
			ID:       container.ID,
			Image:    container.Image,
			Packages: allPackages,
		}
	}

	return result, nil
}

// Copy a single file from a docker container
func copyFileFromDockerContainer(client *client.Client, container types.Container, src string, dst string) error {
	reader, _, err := client.CopyFromContainer(context.Background(), container.ID, src)

	if err != nil {
		return fmt.Errorf("could not find the file %s in docker container %s", src, container.ID)
	}

	defer reader.Close()

	writer, err := os.Create(dst)

	if err != nil {
		panic(err)
	}

	defer writer.Close()

	tarReader := tar.NewReader(reader)
	_, err = tarReader.Next()

	if err != nil {
		panic(err)
	}

	buffer := make([]byte, 16384)
	for {
		read, readErr := tarReader.Read(buffer)

		_, writeErr := writer.Write(buffer[0:read])
		if writeErr != nil {
			panic(writeErr)
		}

		if readErr == io.EOF {
			break
		}
		if readErr != nil {
			panic(err)
		}
	}
	return nil
}
