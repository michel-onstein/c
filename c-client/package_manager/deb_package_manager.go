/*

Copyright 2020 Zededa Inc.

*/
package package_manager

type DebPackageManagerImpl struct{}

func (DebPackageManagerImpl) FilesNeeded() []string {
	return []string{"/var/lib/dpkg/status"}
}

