resource "aws_iam_role" "master" {
    name = "master-${var.stack_name}"
    path = "/"
    assume_role_policy = <<EOF
{
  "Statement": [
    {
      "Principal": {
        "Service": [
          "ec2.amazonaws.com"
        ]
      },
      "Action": [
        "sts:AssumeRole"
      ],
      "Effect": "Allow"
    }
  ],
  "Version": "2012-10-17"
}
EOF
}

resource "aws_iam_role_policy" "master" {
    name = "master"
    role = "${aws_iam_role.master.id}"
    policy = <<EOF
{
  "Statement": [
    {
      "Resource": [
        "arn:aws:s3:::${aws_s3_bucket.exhibitor.id}/*",
        "arn:aws:s3:::${aws_s3_bucket.exhibitor.id}"
      ],
      "Action": [
        "s3:AbortMultipartUpload",
        "s3:DeleteObject",
        "s3:GetBucketAcl",
        "s3:GetBucketPolicy",
        "s3:GetObject",
        "s3:GetObjectAcl",
        "s3:ListBucket",
        "s3:ListBucketMultipartUploads",
        "s3:ListMultipartUploadParts",
        "s3:PutObject",
        "s3:PutObjectAcl"
      ],
      "Effect": "Allow"
    },
    {
      "Resource": "*",
      "Action": [
        "ec2:DescribeKeyPairs",
        "ec2:DescribeSubnets",
        "autoscaling:DescribeLaunchConfigurations",
        "autoscaling:UpdateAutoScalingGroup",
        "autoscaling:DescribeAutoScalingGroups",
        "autoscaling:DescribeScalingActivities",
        "elasticloadbalancing:DescribeLoadBalancers"
      ],
      "Effect": "Allow"
    }
  ],
  "Version": "2012-10-17"
}
EOF
}
