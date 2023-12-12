#! /bin/sh

sudo apt-get update
sudo apt-get install -y curl

# Retrieve Docker installation script and install Docker
sudo curl -ssl https://get.docker.com | sh

# Manage Docker as a non-root user
sudo groupadd docker
sudo usermod -aG docker $USER
newgrp docker

# If permission denied, refer to the link
# https://andy51002000.blogspot.com/2019/02/docker-permission-denied.html