resource "aws_instance" "nat" {
  ami = "${lookup(var.nat_amis, var.aws_region)}"
  instance_type = "m3.medium"
  subnet_id = "${aws_subnet.public.id}"
  source_dest_check = false
  associate_public_ip_address = true
  key_name = "${aws_key_pair.dcos.key_name}"

  vpc_security_group_ids = [
    "${aws_security_group.slave.id}",
    "${aws_security_group.master.id}",
    "${aws_security_group.vpn.id}",
    "${aws_security_group.admin.id}"
  ]

  tags {
    Application = "${var.stack_name}"
    Role = "nat"
  }
}
