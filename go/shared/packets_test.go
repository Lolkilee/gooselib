package shared

import (
	"bytes"
	"strconv"
	"testing"
)

func TestPktHLen(t *testing.T) {
	emptyPkt := NewPacketHeader(0, 0)
	arr, err := HeaderToBytes(emptyPkt)

	if err != nil {
		Err(err)
		t.Fail()
	}

	Log(DEBUG, "length of empty packet header: "+strconv.Itoa(len(arr)))
}

func TestHeaderEncode(t *testing.T) {
	header := NewPacketHeader(128, 0)
	bytes, err := HeaderToBytes(header)
	if err != nil {
		t.Fatalf("failed to convert header: %v", err)
	}

	decodedHeader, err := BytesToHeader(bytes)
	if err != nil {
		t.Fatalf("failed to convert bytes: %v", err)
	}

	if decodedHeader.Version != header.Version {
		t.Errorf("Version mismatch: expected %v, got %v", header.Version, decodedHeader.Version)
	}
	if decodedHeader.Identifier != header.Identifier {
		t.Errorf("Identifier mismatch: expected %v, got %v", header.Identifier, decodedHeader.Identifier)
	}
	if decodedHeader.Length != header.Length {
		t.Errorf("Length mismatch: expected %v, got %v", header.Length, decodedHeader.Length)
	}
}

func TestPktEncode(t *testing.T) {
	header := NewPacketHeader(1, 16)
	data := []byte("test data")

	encodedPacket, err := EncodePacket(header, data)
	if err != nil {
		t.Fatalf("Failed to encode packet: %v", err)
	}
	t.Logf("Encoded packet len: %v", len(encodedPacket))
	t.Logf("Encoded packet: %v", encodedPacket)

	decodedHeader, decodedData, err := DecodePacket(encodedPacket)
	if err != nil {
		t.Fatalf("Failed to decode packet: %v", err)
	}
	t.Logf("Decoded header: %+v", decodedHeader)
	t.Logf("Decoded data: %s", string(decodedData))

	if decodedHeader.Version != header.Version {
		t.Errorf("Version mismatch: expected %v, got %v", header.Version, decodedHeader.Version)
	}
	if decodedHeader.Identifier != header.Identifier {
		t.Errorf("Identifier mismatch: expected %v, got %v", header.Identifier, decodedHeader.Identifier)
	}
	if decodedHeader.Length != header.Length {
		t.Errorf("Length mismatch: expected %v, got %v", header.Length, decodedHeader.Length)
	}

	if !bytes.Equal(decodedData, data) {
		t.Errorf("Data mismatch: expected %v, got %v", data, decodedData)
	}
}
