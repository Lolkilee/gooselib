package main

import (
	"log"
	"net/http"

	"github.com/gin-gonic/gin"
)

func main() {
	r := gin.Default()

	// Default status route without any security (except for TLS)
	r.GET("/", status)
	err := r.RunTLS(":8765", "./creds/server.crt", "./creds/server.key")

	if err != nil {
		log.Fatal(err)
	}
}

func status(c *gin.Context) {
	c.String(http.StatusOK, "service is running")
}
