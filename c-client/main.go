/*

Copyright 2020 Q-Jam B.V.

*/
package main

import (
	"archive/tar"
	"context"
	"encoding/hex"
	"fmt"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/client"
	"io"
	"math/rand"
	"os"
	"path/filepath"

	"q-jam.nl/c/c-client/package_manager"
)

func main() {
	var packageManagers = []package_manager.PackageManager{
		package_manager.ApkPackageManagerImpl{},
		package_manager.DebPackageManagerImpl{},
	}

	cli, err := client.NewClient("unix:///var/run/docker.sock", "v1.22", nil, nil)
	if err != nil {
		panic(err)
	}

	containers, err := cli.ContainerList(context.Background(), types.ContainerListOptions{})
	if err != nil {
		panic(err)
	}

	// Iterate all containers
	for _, container := range containers {
		fmt.Printf("%s %s\n", container.ID[:10], container.Image)

		for _, packageManager := range packageManagers {
			files := packageManager.FilesNeeded()

			// Figure out if all files required by the package manager exist
			allFilesPresent := true
			fileMap := make(map[string]string)
			for _, file := range files {
				temp := TempFileName("df-")

				err = copyFileFromContainer(cli, container, file, temp)
				if err != nil {
					allFilesPresent = false
					break
				}

				fileMap[file] = temp
			}

			if !allFilesPresent {
				continue
			}

			fmt.Printf("Found package manager: %s\n", packageManager.Id())

			// Construct a slice with the temporary files in the right order
			var tempFiles []string
			for _, file := range files {
				tempFiles = append(tempFiles, fileMap[file])
			}

			// Determine the packages
			packages := packageManager.Get(tempFiles)

			for _, pkg := range packages {
				fmt.Printf("\tPackage: %s, Version: %s (%s)\n", pkg.Name, pkg.Version, pkg.Manager)
			}

			// Delete temporary files
			for _, temp := range fileMap {
				err = os.Remove(temp)
				if err != nil {
					panic(err)
				}
			}
		}
	}
}

// Copy a single file from a docker container
func copyFileFromContainer(client *client.Client, container types.Container, src string, dst string) error {
	reader, _, err := client.CopyFromContainer(context.Background(), container.ID, src)

	if err != nil {
		return fmt.Errorf("could not find the file %s in container %s", src, container.ID)
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

func main_package() {
	var packageManagers = []package_manager.PackageManager{
		package_manager.ApkPackageManagerImpl{},
		package_manager.DebPackageManagerImpl{},
	}

	var packageManager package_manager.PackageManager
	for _, packageManager = range packageManagers {
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
			fmt.Printf("Found package manager: %s\n", packageManager.Id())

			packages := packageManager.Get(files)

			for _, pkg := range packages {
				fmt.Printf("Package: %s, Version: %s (%s)\n", pkg.Name, pkg.Version, pkg.Manager)
			}
		}
	}

	//var packages = apk.Get([]string{"./testdata/apk-installed"})
	//
	//var pkg package_manager.Package
	//for _, pkg = range packages {
	//	fmt.Printf("Package: %s, Version: %s (%s)\n", pkg.Name, pkg.Version, pkg.Manager)
	//}
}

// Generate a random temporary filename
func TempFileName(prefix string) string {
	randBytes := make([]byte, 16)
	rand.Read(randBytes)
	return filepath.Join(os.TempDir(), prefix+hex.EncodeToString(randBytes))
}
