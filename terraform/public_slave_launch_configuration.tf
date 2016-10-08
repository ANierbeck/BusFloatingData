resource "aws_launch_configuration" "public_slave" {
  security_groups = ["${aws_security_group.public_slave.id}"]
  image_id = "${lookup(var.coreos_amis, var.aws_region)}"
  instance_type = "${var.public_slave_instance_type}"
  key_name = "${aws_key_pair.dcos.key_name}"
  user_data = "${data.template_file.public_slave_user_data.rendered}"
  associate_public_ip_address = true

  lifecycle {
    create_before_destroy = false
  }
}

data "template_file" "public_slave_user_data" {
  template = "${file("${path.module}/public_slave_user_data.yml")}"

  vars {
    authentication_enabled      = "${var.authentication_enabled}"
    bootstrap_id                = "${var.bootstrap_id}"
    stack_name                  = "${var.stack_name}"
    aws_region                  = "${var.aws_region}"
    cluster_packages            = "${var.cluster_packages}"
    aws_access_key_id           = "${aws_iam_access_key.host_keys.id}"
    aws_secret_access_key       = "${aws_iam_access_key.host_keys.secret}"
    fallback_dns                = "${var.fallback_dns}"
    internal_master_lb_dns_name = "${aws_elb.internal_master.dns_name}"
    public_lb_dns_name          = "${aws_elb.public_slaves.dns_name}"
    exhibitor_s3_bucket         = "${aws_s3_bucket.exhibitor.id}"
    dcos_base_download_url      = "${var.dcos_base_download_url}"
  }
}
