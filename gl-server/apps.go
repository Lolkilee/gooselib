package main

import (
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"io"
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
		if err == nil && err_inp != nil {
			file, err2 := form.File["file"][0].Open()

			if err2 == nil {
				defer file.Close()
				os.MkdirAll(FILES_PATH, os.ModePerm)
				dst, err3 := os.Create(filepath.Join(FILES_PATH, form.File["file"][0].Filename))

				if err3 == nil {
					defer dst.Close()
					_, err4 := io.Copy(dst, file)
					h := sha256.New()

					if err4 != nil {
						res.Success = false
						res.Reason = err4.Error()
					}

					if _, err5 := io.Copy(h, dst); err != nil {
						res.Success = false
						res.Reason = err5.Error()
					}

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
