/*

Copyright 2020 Zededa Inc.

*/
package package_manager

import (
	"bufio"
	"io"
	"log"
	"os"
	"strings"
)

type DebPackageManagerImpl struct{}

// Short name of the package manager
func (DebPackageManagerImpl) Id() string {
	return "deb"
}

// Files needed by the manager
func (DebPackageManagerImpl) FilesNeeded() []string {
	return []string{"/var/lib/dpkg/status"}
}

// Get the packages installed according to the supplied files
func (DebPackageManagerImpl) Get(files []string) []Package {
	file, err := os.Open(files[0])
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()

	reader := bufio.NewReader(file)

	var packages []Package

	var name string
	var version string
	var installed bool

	for {
		line, err := reader.ReadString('\n')
		if err != nil && err != io.EOF {
			break
		}

		line = strings.TrimSuffix(line, "\n")

		if strings.HasPrefix(line, "Package: ") {
			var split []string = strings.SplitN(line, " ", 2)

			name = split[1]
		} else if strings.HasPrefix(line, "Version: ") {
			var split []string = strings.SplitN(line, " ", 2)

			version = split[1]
		} else if strings.HasPrefix(line, "Status: ") {
			var split []string = strings.SplitN(line, " ", 2)

			status := split[1]

			// TODO Check if this is enough
			if status == "install ok installed" {
				installed = true
			}
		}

		// End of package definition?
		if (line == "" || err != nil) && installed {
			packages = append(packages,
				Package{
					Name:    name,
					Version: version,
					Manager: "deb",
				},
			)

			installed = false
		}

		if err != nil {
			break
		}
	}

	return packages
}
