resource "aws_s3_bucket" "exhibitor" {
  bucket = "netflix-exhibitor-42"
  force_destroy = true

  lifecycle {
    prevent_destroy = false
  }
}
