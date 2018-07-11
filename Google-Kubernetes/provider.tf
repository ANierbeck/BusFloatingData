// Configure the Google Cloud provider
provider "google" {
  credentials = "${file("account.json")}"
  project     = "linen-age-193411"
  region      = "europe-west1"
}
