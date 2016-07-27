resource "aws_s3_bucket" "exhibitor" {
  bucket = "netflix-exhibitor-23"
  force_destroy = true

  lifecycle {
    prevent_destroy = false
  }
}
