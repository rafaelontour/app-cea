package main

import (
	"errors"
	"flag"
	"fmt"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"strings"
)

func main() {
	addr := flag.String("addr", ":8080", "HTTP listen address")
	imagesDirFlag := flag.String("images-dir", "", "directory containing exercise image folders")
	flag.Parse()

	imagesDir, err := resolveImagesDir(*imagesDirFlag)
	if err != nil {
		log.Fatal(err)
	}

	server := &imageServer{imagesDir: imagesDir}
	mux := http.NewServeMux()
	mux.HandleFunc("/health", server.health)
	mux.HandleFunc("/exercise-image/", server.exerciseImage)

	log.Printf("CEA image backend listening on %s", *addr)
	log.Printf("Serving images from %s", imagesDir)
	log.Fatal(http.ListenAndServe(*addr, mux))
}

type imageServer struct {
	imagesDir string
}

func (s imageServer) health(w http.ResponseWriter, _ *http.Request) {
	withCors(w)
	w.Header().Set("Content-Type", "application/json")
	_, _ = w.Write([]byte(`{"status":"ok"}`))
}

func (s imageServer) exerciseImage(w http.ResponseWriter, r *http.Request) {
	withCors(w)
	if r.Method == http.MethodOptions {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	if r.Method != http.MethodGet && r.Method != http.MethodHead {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}

	relativePath := strings.TrimPrefix(r.URL.Path, "/exercise-image/")
	if relativePath == "" {
		http.Error(w, "missing image path", http.StatusBadRequest)
		return
	}

	filePath, err := safeJoin(s.imagesDir, relativePath)
	if err != nil {
		http.Error(w, "invalid image path", http.StatusBadRequest)
		return
	}

	http.ServeFile(w, r, filePath)
}

func resolveImagesDir(value string) (string, error) {
	if value != "" {
		return mustExistingDir(value)
	}
	if env := os.Getenv("CEA_IMAGES_DIR"); env != "" {
		return mustExistingDir(env)
	}

	candidates := []string{
		filepath.Join("app", "src", "main", "assets", "images"),
		filepath.Join("..", "app", "src", "main", "assets", "images"),
		filepath.Join("..", "..", "app", "src", "main", "assets", "images"),
	}
	for _, candidate := range candidates {
		if dir, err := mustExistingDir(candidate); err == nil {
			return dir, nil
		}
	}

	return "", errors.New("images directory not found; pass -images-dir or set CEA_IMAGES_DIR")
}

func mustExistingDir(value string) (string, error) {
	dir, err := filepath.Abs(value)
	if err != nil {
		return "", err
	}
	info, err := os.Stat(dir)
	if err != nil {
		return "", err
	}
	if !info.IsDir() {
		return "", fmt.Errorf("%s is not a directory", dir)
	}
	return dir, nil
}

func safeJoin(root string, relativePath string) (string, error) {
	clean := filepath.Clean(filepath.FromSlash(relativePath))
	if clean == "." || strings.HasPrefix(clean, ".."+string(filepath.Separator)) || clean == ".." || filepath.IsAbs(clean) {
		return "", errors.New("path escapes image root")
	}
	if filepath.Ext(clean) != ".jpg" {
		return "", errors.New("only jpg images are served")
	}

	fullPath := filepath.Join(root, clean)
	relativeToRoot, err := filepath.Rel(root, fullPath)
	if err != nil {
		return "", err
	}
	if strings.HasPrefix(relativeToRoot, ".."+string(filepath.Separator)) || relativeToRoot == ".." {
		return "", errors.New("path escapes image root")
	}
	return fullPath, nil
}

func withCors(w http.ResponseWriter) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
}
