package main

import (
	"github.com/boltdb/bolt"
	"github.com/gofiber/fiber/v2"
)

// Helper function to make authenticating requests easy
func Authenticate(c *fiber.Ctx, db *bolt.DB) bool {
	//user := c.GetRespHeader("user")
	//pass := c.GetRespHeader("pass") // plain text password (TLS handles encryption)

	return false
}
