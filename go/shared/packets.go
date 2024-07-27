package shared

import (
	"bytes"
	"encoding/gob"
	"errors"
)

const (
	LATEST_PKT_VER = 1
)

type GLPacketHeader struct {
	Version    uint8
	Identifier uint8
	Length     uint16
}

func NewPacketHeader(id uint8, len uint16) *GLPacketHeader {
	ph := GLPacketHeader{Version: LATEST_PKT_VER, Identifier: id, Length: len}
	return &ph
}

func EncodePacket(header *GLPacketHeader, data []byte) ([]byte, error) {
	hBytes, err := HeaderToBytes(header)
	if err != nil {
		return nil, err
	}

	hLen := len(hBytes)
	totalLen := hLen + len(data)
	arr := make([]byte, totalLen)
	for i := 0; i < totalLen; i++ {
		if i < hLen {
			arr[i] = hBytes[i]
		} else {
			arr[i] = data[i-hLen]
		}
	}

	return arr, nil
}

func DecodePacket(input []byte) (*GLPacketHeader, []byte, error) {
	var ph *GLPacketHeader
	var data []byte
	var err error

	emptyPkt := NewPacketHeader(0, 0)
	arr, err := HeaderToBytes(emptyPkt)

	if err != nil {
		Log(DEBUG, "first err")
		return nil, nil, err
	}

	phSize := len(arr)
	if phSize > len(input) || len(input) < 0 {
		Log(DEBUG, "second err")
		return nil, nil, errors.New("invalid input array during decoding")
	}

	headerPart := input[:phSize]
	data = input[phSize:]
	ph, err = BytesToHeader(headerPart)

	return ph, data, err
}

func HeaderToBytes(u *GLPacketHeader) ([]byte, error) {
	var buf bytes.Buffer
	enc := gob.NewEncoder(&buf)
	err := enc.Encode(u)
	if err != nil {
		Log(DEBUG, "serialization err")
		return nil, err
	}
	return buf.Bytes(), nil
}

func BytesToHeader(data []byte) (*GLPacketHeader, error) {
	buf := bytes.NewBuffer(data)
	dec := gob.NewDecoder(buf)
	var ph GLPacketHeader
	err := dec.Decode(&ph)
	if err != nil {
		Log(DEBUG, "deserialization err")
		return nil, err
	}
	return &ph, nil
}
