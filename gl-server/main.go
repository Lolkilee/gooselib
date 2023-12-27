package main

import (
	"os"

	"github.com/boltdb/bolt"
	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/log"
)

const DB_PATH = "./data.db"
const CERT_PATH = "./creds/cert.pem"
const KEY_PATH = "./creds/cert.key"

var glob_db *bolt.DB
var admin_pass string // only exists in runtime

func status(c *fiber.Ctx) error {
	return c.SendString("gooselib server is running")
}

func login(c *fiber.Ctx) error {
	if authenticate(c, glob_db) {
		c.Status(fiber.StatusOK)
		return c.SendString("Authentication passed")
	} else {
		c.Status(fiber.StatusForbidden)
		return c.SendString("Authentication failed")
	}
}

func init_admin_pass() {
	if len(os.Args) == 2 {
		admin_pass = os.Args[1]
		log.Info("Set admin password to: " + admin_pass)
	} else {
		admin_pass = "password"
		log.Info("No password argument detected! setting admin password to 'password'")
	}
}

func main() {
	init_admin_pass()

	// Start database
	db, err := bolt.Open(DB_PATH, 0600, nil)
	if err != nil {
		log.Fatal(err)
		return
	}
	defer db.Close()
	glob_db = db
	init_db(glob_db)

	app := fiber.New()

	// Endpoints
	app.Get("/", status)
	app.Get("/login", login)
	app.Post("/create-user", create_user_ep)
	app.Post("/delete-user", remove_user_ep)

	// Serve with TLS
	log.Fatal(app.ListenTLS(":8765", CERT_PATH, KEY_PATH))
}
