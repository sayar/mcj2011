__author__ = 'ramisayar'

from django.conf.urls.defaults import patterns, include, url
from django.views.generic.simple import direct_to_template

import views

urlpatterns = patterns('',
    url(r'^$', views.view_sms, name='view_sms'),
    url(r'^post_sms/$', views.post_sms, name='post_sms')
)
