resource "aws_s3_bucket" "exhibitor" {
  bucket = "netflix-exhibitor-${var.exhibitor_uid}"
  force_destroy = true

  lifecycle {
    prevent_destroy = false
  }
}
