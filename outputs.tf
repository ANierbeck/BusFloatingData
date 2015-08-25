output "master" {
  value = "${aws_elb.dcos.dns_name}"
}

output "public_slave" {
  value = "${aws_elb.public_slaves.dns_name}"
}
