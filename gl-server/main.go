package main

import (
	"log"

	"github.com/boltdb/bolt"
	"github.com/gofiber/fiber/v2"
)

const DB_PATH = "./data.db"
const CERT_PATH = "./creds/cert.pem"
const KEY_PATH = "./creds/cert.key"

var glob_db *bolt.DB

func status(c *fiber.Ctx) error {
	return c.SendString("gooselib server is running")
}

func main() {
	// Start database
	db, err := bolt.Open(DB_PATH, 0600, nil)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()
	glob_db = db
	init_db(glob_db)

	app := fiber.New()

	// Endpoints
	app.Get("/", status)

	// Serve with TLS
	log.Fatal(app.ListenTLS(":8765", CERT_PATH, KEY_PATH))
}
