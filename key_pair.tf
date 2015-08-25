resource "aws_key_pair" "dcos" {
  key_name = "dcos-main"
  public_key = "${var.ssh_public_key}"
}
