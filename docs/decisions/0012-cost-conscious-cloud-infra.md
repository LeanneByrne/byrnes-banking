# 0012 — Cost-conscious infrastructure choices for a demo deployment

**Status:** Accepted

## Context

Every AWS resource that exists costs money for existing, not just for being used. A handful of
standard production patterns each carry a real, continuous dollar cost that a demo deployment
doesn't need to pay to prove the point it exists to prove.

## Decisions, and the production pattern each one departs from

**No NAT Gateway.** Production practice: application tasks live in private subnets with no
public IP, reaching the internet (to pull images, call external APIs) through a NAT Gateway.
Here: the ECS task runs in a *public* subnet with a public IP, reachable only through a security
group that permits inbound traffic from the load balancer alone. A NAT Gateway bills hourly for
existing, independent of traffic (~$32+/month) — disproportionate for a task whose only outbound
need is pulling its own image from ECR. The security boundary that matters (nothing can reach
the app except through the load balancer) is preserved by the security group either way; what's
given up is defense-in-depth against the app's *own* outbound traffic being redirected, which
isn't a meaningfully different risk for a stateless container with no secrets baked into it.

**Single-AZ RDS, 1-day backup retention.** Production practice: Multi-AZ with automatic failover
and a multi-week (or longer) backup retention window, both billed continuously for the standby
capacity and storage they hold. Here: single instance, minimal retention. This demo's data isn't
something a real outage would make anyone's week worse — the seed data is fake, and it can be
recreated by Flyway from scratch in minutes. A real banking system obviously cannot make this
choice; its data has to survive an AZ failure.

**No custom domain / ACM certificate for the backend.** The ALB serves plain HTTP on its default
`*.elb.amazonaws.com` hostname; only the frontend (via CloudFront, which provides HTTPS on its
default domain for free) gets HTTPS. A real deployment handling actual credentials over the
network would need TLS on the API too — ACM plus a real domain this project doesn't have would
fix that, and is explicitly out of scope here, documented rather than silently skipped.

**`db.t4g.micro`, 256 CPU units / 512MB Fargate task, `desired_count = 1`.** The smallest
instance/task sizes available, with no redundancy. This demo isn't being load-tested or expected
to survive a single-task restart gracefully; a real system sizes for its actual expected load
and failure tolerance, which this one deliberately doesn't attempt to simulate.

## Consequence

None of this is how the same application would actually be deployed for real users. Every
decision above is reversible by changing a handful of Terraform values (`multi_az = true`, an
ACM certificate + a real domain, a NAT Gateway, larger instance classes, `desired_count = 2`) —
the architecture doesn't have to change, only the dial settings, which is itself evidence the
underlying design (security groups as the real boundary, stateless tasks, a managed database)
was sound regardless of which cost tier it's running at.
