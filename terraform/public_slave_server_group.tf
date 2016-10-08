resource "aws_autoscaling_group" "public_slave_server_group" {
  name = "Public-Slaves-${var.stack_name}"

  min_size = "${var.public_slave_instance_count}"
  max_size = "${var.public_slave_instance_count}"
  desired_capacity = "${var.public_slave_instance_count}"

  load_balancers = ["${aws_elb.public_slaves.id}"]
  availability_zones = ["${aws_subnet.public.availability_zone}"]
  vpc_zone_identifier = ["${aws_subnet.public.id}"]
  launch_configuration = "${aws_launch_configuration.public_slave.id}"

  tag {
    key = "role"
    value = "mesos-slave"
    propagate_at_launch = true
  }

  lifecycle {
    create_before_destroy = false
  }
}
