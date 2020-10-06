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

	var packages = deb.Get([]string{"./testdata/status"})

	var pkg package_manager.Package
	for _, pkg = range packages {
		fmt.Printf("Package: %s, Version: %s\n",pkg.Name, pkg.Version)
	}
}
