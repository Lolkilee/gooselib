package main

import (
	"bytes"
	"encoding/gob"
	"errors"

	"github.com/boltdb/bolt"
	"github.com/gofiber/fiber/v2/log"
)

// Helper functions for the database

/*
	buckets

users -- contains login information of all users (username, password)
*/
var DB_BUCKET_NAMES = [...]string{"users"}

// creates necessary buckets if they dont exist yet
func init_db(db *bolt.DB) {
	for i := 0; i < len(DB_BUCKET_NAMES); i++ {
		init_bucket(db, DB_BUCKET_NAMES[i])
	}
}

func init_bucket(db *bolt.DB, name string) {
	db.Update(func(tx *bolt.Tx) error {
		_, err := tx.CreateBucketIfNotExists([]byte(name))
		if err != nil {
			log.Warn("Error creating bucket")
			log.Warn(err)
		}
		return nil
	})
}

func db_get[T any](db *bolt.DB, bucket string, key string) (T, error) {
	var ret_val T
	err := db.View(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte(bucket))
		raw_val := b.Get([]byte(key))

		if raw_val != nil {
			buf := bytes.NewBuffer(raw_val)
			var val T
			dec := gob.NewDecoder(buf)
			if err := dec.Decode(&val); err != nil {
				return errors.New("database parse error")
			}

			ret_val = val
		}

		return errors.New("database key error")
	})

	return ret_val, err
}

func db_set[T any](db *bolt.DB, bucket string, key string, val T) error {
	var buf bytes.Buffer
	enc := gob.NewEncoder(&buf)
	if err := enc.Encode(val); err != nil {
		log.Warn(err)
		return errors.New("database encode error")
	}

	return db.Update(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte(bucket))
		err := b.Put([]byte(key), buf.Bytes())
		return err
	})
}
