resource "aws_autoscaling_group" "master_server_group" {
  name = "Masters-${var.stack_name}"

  min_size = "${var.master_instance_count}"
  max_size = "${var.master_instance_count}"
  desired_capacity = "${var.master_instance_count}"

  load_balancers = ["${aws_elb.internal_master.id}"]

  availability_zones = ["${aws_subnet.master.availability_zone}"]
  vpc_zone_identifier = ["${aws_subnet.master.id}"]
  launch_configuration = "${aws_launch_configuration.master.id}"

  tag {
    key = "role"
    value = "mesos-master"
    propagate_at_launch = true
  }

  lifecycle {
    create_before_destroy = false
  }
}
