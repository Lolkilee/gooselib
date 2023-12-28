package main

// https://gist.github.com/mimoo/25fc9716e0f1353791f5908f94d6e726

import (
	"archive/tar"
	"bytes"
	"compress/gzip"
	"crypto/tls"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"os"
	"path/filepath"
)

// command:
// ./gl-packager <folder> <admin password> <app name> <version name> <url>
func main() {
	if len(os.Args) == 6 {
		folder := os.Args[1]
		pass := os.Args[2]
		name := os.Args[3]
		version := os.Args[4]
		url := os.Args[5]

		var buf bytes.Buffer
		err := compress(folder, &buf)
		if err != nil {
			panic(err)
		}

		path := "./" + name + "-" + version + ".app"
		file, err := os.OpenFile(path, os.O_CREATE|os.O_RDWR, os.FileMode(600))
		if err != nil {
			panic(err)
		}
		defer file.Close()

		if _, err := io.Copy(file, &buf); err != nil {
			panic(err)
		}

		body := &bytes.Buffer{}
		writer := multipart.NewWriter(body)

		part, err := writer.CreateFormFile("file", file.Name())
		if err != nil {
			panic(err)
		}

		_, err = io.Copy(part, file)
		if err != nil {
			panic(err)
		}

		writer.Close()

		fi, err := file.Stat()
		if err != nil {
			panic(err)
		}
		file_size := fi.Size()

		req, err := http.NewRequest("POST", url+"/upload", body)
		if err != nil {
			panic(err)
		}

		req.Header.Set("Content-Type", writer.FormDataContentType())
		req.Header.Set("pass", pass)
		req.Header.Set("name", name)
		req.Header.Set("version", version)
		req.Header.Set("size", fmt.Sprint(file_size))

		// Ignore invalid certificates (for test purposes)
		http.DefaultTransport.(*http.Transport).TLSClientConfig = &tls.Config{InsecureSkipVerify: true}
		client := &http.Client{}
		resp, err := client.Do(req)
		if err != nil {
			panic(err)
		}
		defer resp.Body.Close()
		fmt.Println("Server response: ", resp.Status)
		b, err := io.ReadAll(resp.Body)
		if err != nil {
			panic(err)
		}

		fmt.Println(string(b))

	} else {
		fmt.Println("Invalid argument count command format: ./gl-packager <folder> <admin password> <app name> <version name> <url>")
		fmt.Println("Found " + fmt.Sprint(len(os.Args)) + " args")
	}
}

func compress(src string, buf io.Writer) error {
	// tar > gzip > buf
	zr := gzip.NewWriter(buf)
	tw := tar.NewWriter(zr)

	// walk through every file in the folder
	filepath.Walk(src, func(file string, fi os.FileInfo, err error) error {
		// generate tar header
		header, err := tar.FileInfoHeader(fi, file)
		if err != nil {
			return err
		}

		// must provide real name
		// (see https://golang.org/src/archive/tar/common.go?#L626)
		header.Name = filepath.ToSlash(file)

		// write header
		if err := tw.WriteHeader(header); err != nil {
			return err
		}
		// if not a dir, write file content
		if !fi.IsDir() {
			data, err := os.Open(file)
			if err != nil {
				return err
			}
			if _, err := io.Copy(tw, data); err != nil {
				return err
			}
		}
		return nil
	})

	// produce tar
	if err := tw.Close(); err != nil {
		return err
	}
	// produce gzip
	if err := zr.Close(); err != nil {
		return err
	}
	//
	return nil
}
