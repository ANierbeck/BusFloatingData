resource "aws_instance" "vpn" {
  vpc_security_group_ids = [
    "${aws_security_group.admin.id}",
    "${aws_security_group.vpn.id}",
    "${aws_security_group.master.id}",
    "${aws_security_group.slave.id}",
    "${aws_security_group.public_slave.id}"
  ]

  subnet_id = "${aws_subnet.public.id}"

  ami = "${lookup(var.vpn_amis, var.aws_region)}"
  instance_type = "${var.vpn_instance_type}"
  key_name = "${aws_key_pair.dcos.key_name}"
  user_data = "${template_file.vpn_user_data.rendered}"
  associate_public_ip_address = true

  tags {
    Application = "${var.stack_name}"
    Role = "vpn"
  }

  lifecycle {
    create_before_destroy = false
  }
}

resource "template_file" "vpn_user_data" {
  template = "${file("${path.module}/vpn_user_data.yml")}"

  vars {
    admin_user  = "${var.openvpn_admin_user}"
    admin_pw    = "${var.openvpn_admin_pw}"
  }
}
