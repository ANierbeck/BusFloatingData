resource "aws_s3_bucket" "exhibitor" {
  bucket = "dcos-exhibitor"
  force_destroy = true

  lifecycle {
    prevent_destroy = false
  }
}
