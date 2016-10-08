resource "aws_key_pair" "dcos" {
  key_name = "dcos-${var.stack_name}"
  public_key = "${var.ssh_public_key}"
}
