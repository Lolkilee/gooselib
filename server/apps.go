package main

import (
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"io"
	"mime/multipart"
	"os"
	"path/filepath"
	"strconv"

	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/log"
)

var FILES_PATH = "./files/"

func get_apps(c *fiber.Ctx) error {
	apps := db_get_keys(glob_db, DB_APPS)
	return c.JSON(apps)
}

func create_app_version(name string, version_name string,
	version_hash string, file_size int64) {

	log.Info("Creating new app: " + name)
	log.Info("version: " + version_name)
	log.Info("hash: " + version_hash)
	log.Info("size: " + fmt.Sprint(file_size))

	ver := AppVersion{
		VersionName: version_name,
		Hash:        version_hash,
		FileSize:    file_size,
	}

	if db_contains_key(glob_db, DB_APPS, name) {
		cur, err := db_get[App](glob_db, DB_APPS, name)
		if err != nil {
			log.Warn(err)
			return
		}
		cur.Versions = append(cur.Versions, ver)
		db_set[App](glob_db, DB_APPS, name, cur)
	} else {
		new_app := App{Name: name, Versions: []AppVersion{ver}}
		db_set[App](glob_db, DB_APPS, name, new_app)
	}
}

func print_header_info(file_h *multipart.FileHeader) {
	log.Info("Receiving file, printing info:")
	log.Info("Filename:", file_h.Filename)
	log.Info("Size:", file_h.Size)
	log.Info("Content-Type:", file_h.Header.Get("Content-Type"))
	log.Info("Content-Disposition:", file_h.Header.Get("Content-Disposition"))
	log.Info("Content-Transfer-Encoding:", file_h.Header.Get("Content-Transfer-Encoding"))
}

func upload_app(c *fiber.Ctx) error {
	res := FailableResponse{
		Success: true,
	}

	if admin_pass == c.Get("pass") {
		app_name := c.Get("name")
		ver_name := c.Get("version")
		file_size, err_inp := strconv.ParseInt(c.Get("size"), 10, 64)

		var hash string

		form, err := c.MultipartForm()

		if err == nil && err_inp == nil {
			file, err2 := form.File["file"][0].Open()
			print_header_info(form.File["file"][0])

			if err2 == nil {
				defer file.Close()
				os.MkdirAll(FILES_PATH, os.ModePerm)
				dst, err3 := os.Create(filepath.Join(FILES_PATH, form.File["file"][0].Filename))

				if err3 == nil {

					defer dst.Close()
					// Hash the file content
					h := sha256.New()
					if _, err4 := io.Copy(h, file); err4 != nil {
						log.Warn(err4.Error())
						res.Success = false
						res.Reason = err4.Error()
					}

					// Seek back to the beginning of the file before hashing
					if _, err5 := file.Seek(0, io.SeekStart); err5 != nil {
						res.Success = false
						res.Reason = err5.Error()
					}

					// Copy the file content to the destination file
					amount, err6 := io.Copy(dst, file)
					if err6 != nil {
						res.Success = false
						res.Reason = err6.Error()
					}

					log.Info("Wrote " + fmt.Sprint(amount) + " bytes")

					hash = base64.URLEncoding.EncodeToString(h.Sum(nil))

				} else {
					res.Success = false
					res.Reason = err3.Error()
				}
			} else {
				res.Success = false
				res.Reason = err2.Error()
			}
		} else if err != nil {
			res.Success = false
			res.Reason = err.Error()
		} else if err_inp != nil {
			res.Success = false
			res.Reason = err_inp.Error()
		}

		if res.Success {
			create_app_version(app_name, ver_name, hash, file_size)
		}
	} else {
		res.Success = false
		res.Reason = "Invalid admin password"
	}

	return c.JSON(res)
}

func app_contains_version(a *App, v string) bool {
	for i := 0; i < len(a.Versions); i++ {
		if a.Versions[i].VersionName == v {
			return true
		}
	}

	return false
}

func app_get_version(a *App, v string) AppVersion {
	for i := 0; i < len(a.Versions); i++ {
		if a.Versions[i].VersionName == v {
			return a.Versions[i]
		}
	}

	return AppVersion{}
}

func parse_range(range_h string, f_size int64) (start, end int64, err error) {
	n, err := fmt.Sscanf(range_h, "bytes=%d-%d", &start, &end)
	if err != nil || n != 2 {
		return 0, 0, fmt.Errorf("invalid range format")
	}

	if end == 0 {
		end = f_size - 1
	}

	return start, end, nil
}

func download_app(c *fiber.Ctx) error {
	if authenticate(c, glob_db) {
		app_name := c.Get("app")
		app_version := c.Get("version")

		if db_contains_key(glob_db, DB_APPS, app_name) && app_name != "" && app_version != "" {
			app, err := db_get[App](glob_db, DB_APPS, app_name)
			if err != nil {
				return c.SendString("error retrieving app")
			}

			if !app_contains_version(&app, app_version) {
				return c.SendString("invalid version")
			}

			app_ver := app_get_version(&app, app_version)

			path := FILES_PATH + app_name + "-" + app_ver.VersionName + ".app"
			file, err := os.Open(path)
			if err != nil {
				return c.SendString(err.Error())
			}
			defer file.Close()

			file_info, err := file.Stat()
			if err != nil {
				return c.SendString(err.Error())
			}

			range_h := c.Get("Range")

			if range_h != "" {
				start, end, err := parse_range(range_h, file_info.Size())
				if err != nil {
					c.SendString(err.Error())
				}

				c.Set("Content-Range", fmt.Sprintf("bytes %d-%d/%d", start, end, file_info.Size()))
				c.Status(fiber.StatusPartialContent)

				_, err = file.Seek(start, 0)
				if err != nil {
					return err
				}

				return c.SendStream(file, (int)(end-start+1))
			}

			// Send whole file if no range header is present
			c.SendFile(path)
			return nil
		} else {
			return c.SendString("invalid app name")
		}
	} else {
		c.Status(fiber.StatusForbidden)
		return c.SendString("invalid authentication")
	}
}
