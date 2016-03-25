resource "aws_instance" "dcos" {
  vpc_security_group_ids = [
    "${aws_security_group.master.id}",
    "${aws_security_group.admin.id}"
  ]

  subnet_id = "${aws_subnet.master.id}"

  ami = "${lookup(var.ubuntu_amis, var.aws_region)}"
  instance_type = "m3.medium"
  key_name = "${aws_key_pair.dcos.key_name}"
  user_data = "${template_file.dcos_user_data.rendered}"
  associate_public_ip_address = false

  tags {
    Application = "${var.stack_name}"
    Role = "dcos"
  }

  lifecycle {
    create_before_destroy = false
  }
}

resource "template_file" "dcos_user_data" {
  template = "${file("${path.module}/dcos_user_data.sh")}"

  vars {
    internal_master_lb_dns_name = "${aws_elb.internal_master.dns_name}"
  }
}
