resource "google_container_cluster" "gcp_kubernetes" {
  name               = "${var.cluster_name}"
  zone               = "europe-west1-b"
  initial_node_count = "${var.gcp_cluster_count}"

  node_config {
    machine_type = "n1-standard-2"
    oauth_scopes = [
      "https://www.googleapis.com/auth/compute",
      "https://www.googleapis.com/auth/devstorage.read_only",
      "https://www.googleapis.com/auth/logging.write",
      "https://www.googleapis.com/auth/monitoring",
    ]

    labels {
      this-is-for = "dev-cluster"
    }

    tags = ["dev", "work"]
  }

  master_auth {
    username = "${var.linux_admin_username}"
    password = "${var.linux_admin_password}}"
  }

  min_master_version = "${var.gcp_master_version}"

  provisioner "local-exec" {
    command = "echo kubernetes created"
  }

#  provisioner "local-exec" {
#    command = "./pods/initCluster.sh"
#  }
}