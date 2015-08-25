resource "aws_launch_configuration" "master" {
  iam_instance_profile = "${aws_iam_instance_profile.master.id}"
  security_groups = [
    "${aws_security_group.master.id}",
    "${aws_security_group.admin.id}"
  ]
  image_id = "${var.instance_ami}"
  instance_type = "${var.master_instance_type}"
  key_name = "${aws_key_pair.dcos.key_name}"
  user_data = "${template_file.master_user_data.rendered}"
  associate_public_ip_address = true

  root_block_device {
    volume_type = "gp2"
    volume_size = "64"
    delete_on_termination = true
  }

  lifecycle {
    create_before_destroy = false
  }
}

resource "template_file" "master_user_data" {
  filename = "master_user_data.yml"

  vars {
    stack_name                  = "${var.stack_name}"
    aws_region                  = "${var.aws_region}"
    aws_access_key_id           = "${aws_iam_access_key.host_keys.id}"
    aws_secret_access_key       = "${aws_iam_access_key.host_keys.secret}"
    fallback_dns                = "${var.fallback_dns}"
    internal_master_lb_dns_name = "${aws_elb.internal_master.dns_name}"
    dcos_lb_dns_name            = "${aws_elb.dcos.dns_name}"
    exhibitor_s3_bucket         = "${aws_s3_bucket.exhibitor.id}"
    bootstrap_repo_root         = "${var.bootstrap_repo_root}"
    mesos_quorum                = "${var.master_quorum_count}"
    master_instance_count       = "${var.master_instance_count}"
  }
}
