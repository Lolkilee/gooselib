package main

import (
	"crypto/tls"
	"fmt"
	"io"
	"net/http"
	"os"
)

const downloadedFilePath = "temp.app"

// args: [url] [path] [username] [password] [app name] [app version]
func main() {

	if len(os.Args) == 7 {
		url := os.Args[1] + "/download"
		//path := os.Args[2]
		username := os.Args[3]
		pass := os.Args[4]
		app := os.Args[5]
		version := os.Args[6]

		fmt.Println(url)
		fmt.Println(username)
		fmt.Println(pass)

		var startRange int64
		file, err := os.OpenFile(downloadedFilePath, os.O_CREATE|os.O_WRONLY, 0644)
		if err != nil {
			panic(err)
		}
		defer file.Close()

		// Check if the file already exists and get its size
		fileInfo, err := file.Stat()
		if err == nil {
			startRange = fileInfo.Size()
		}

		http.DefaultTransport.(*http.Transport).TLSClientConfig = &tls.Config{InsecureSkipVerify: true}
		client := &http.Client{}

		req, err := http.NewRequest("GET", url, nil)
		if err != nil {
			panic(err)
		}

		req.Header.Set("Range", fmt.Sprintf("bytes=%d-", startRange))
		req.Header.Set("user", username)
		req.Header.Set("pass", pass)
		req.Header.Set("app", app)
		req.Header.Set("version", version)

		resp, err := client.Do(req)
		if err != nil {
			panic(err)
		}
		defer resp.Body.Close()

		if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusPartialContent {
			panic("Failed to download file")
		}

		_, err = file.Seek(startRange, 0)
		if err != nil {
			panic(err)
		}

		_, err = io.Copy(file, resp.Body)
		if err != nil {
			panic(err)
		}

		// Print the total downloaded size
		totalDownloaded := startRange + resp.ContentLength
		fmt.Printf("File downloaded successfully. Total downloaded: %s\n", formatFileSize(totalDownloaded))
	} else {
		fmt.Println("invalid args, args: [url] [path] [username] [password] [app name] [app version]")
	}
}

// formatFileSize formats file size in human-readable format
func formatFileSize(size int64) string {
	const (
		_  = iota
		KB = 1 << (10 * iota)
		MB
		GB
		TB
	)

	switch {
	case size < KB:
		return fmt.Sprintf("%d B", size)
	case size < MB:
		return fmt.Sprintf("%.2f KB", float64(size)/float64(KB))
	case size < GB:
		return fmt.Sprintf("%.2f MB", float64(size)/float64(MB))
	case size < TB:
		return fmt.Sprintf("%.2f GB", float64(size)/float64(GB))
	default:
		return fmt.Sprintf("%.2f TB", float64(size)/float64(TB))
	}
}
