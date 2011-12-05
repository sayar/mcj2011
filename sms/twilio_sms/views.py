from django.http import Http404, HttpResponseRedirect
from django.contrib.auth.decorators import login_required
from django.shortcuts import redirect, render_to_response, get_object_or_404
from django.template.context import RequestContext
from django_twilio.decorators import twilio_view
from twilio_sms.models import SMS
from twilio.twiml import Response

def view_sms(request):
    messages = SMS.objects.order_by('-created')[:50]
    return render_to_response('twilio_sms/view_sms.html', {
        'messages': messages
    }, context_instance=RequestContext(request))

@twilio_view
def post_sms(request):
    """
    The twilio view decoractor ensures that only valid twilio requests are passed to this method.
    """
    message = SMS(smssid=request.POST.get('SmsSid'), from_number=request.POST.get('From'), \
        to_number=request.POST.get('To'), body=request.POST.get('Body'))
    message.save()
