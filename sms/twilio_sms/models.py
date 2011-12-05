from django.contrib.auth.models import User
from django.db import models
from django_extensions.db.models import TimeStampedModel
from django.utils.translation import ugettext_lazy as _

# Notes:
# null=False should be used for all char and text fields.

class SMS(TimeStampedModel):
    smssid = models.CharField(max_length=34, blank=False, null=False, editable=False)
    from_number = models.CharField(max_length=30, blank=False, null=False, editable=False)
    to_number = models.CharField(max_length=30, blank=False, null=False, editable=False)
    body = models.CharField(max_length=160, blank=False, null=False, editable=False)