/*

Copyright 2020 Q-Jam B.V.

*/
package main

import (
	"fmt"
	"q-jam.nl/c/c-client/package_manager"
)

func main() {

	var deb package_manager.PackageManager = package_manager.DebPackageManagerImpl{}

	for _, file := range deb.FilesNeeded() {
		fmt.Printf("File: %s", file)
	}

}
