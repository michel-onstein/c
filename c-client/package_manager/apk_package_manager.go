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

type ApkPackageManagerImpl struct{}

// Short name of the package manager
func (ApkPackageManagerImpl) Id() string {
	return "apk"
}

func (ApkPackageManagerImpl) FilesNeeded() []string {
	return []string{"/lib/apk/db/installed"}
}

func (ApkPackageManagerImpl) Get(files []string) []Package {
	file, err := os.Open(files[0])
	if err != nil {
		log.Fatal(err)
	}
	defer file.Close()

	reader := bufio.NewReader(file)
	var line string

	var packages []Package

	var name string
	var version string

	for {
		line, err = reader.ReadString('\n')
		if err != nil && err != io.EOF {
			break
		}

		line = strings.TrimSuffix(line, "\n")

		if line != "" {
			if strings.HasPrefix(line, "P:") {
				var split []string = strings.SplitN(line, ":", 2)

				name = split[1]
			} else if strings.HasPrefix(line, "V:") {
				var split []string = strings.SplitN(line, ":", 2)

				version = split[1]
			}
		}

		// End of package definition?
		if line == "" || err != nil {
			packages = append(packages,
				Package{
					Name:    name,
					Version: version,
					Manager: "apk",
				},
			)
		}

		if err != nil {
			break
		}
	}

	return packages
}
