echo "------------------------------------------------------------"
echo "Installing Python Environment"
echo "------------------------------------------------------------"
sudo easy_install virtualenv
sudo easy_install pip
sudo pip install virtualenvwrapper
export WORKON_HOME=~/.virtualenvs
mkdir -p $WORKON_HOME
source /usr/local/bin/virtualenvwrapper.sh

echo "------------------------------------------------------------"
echo "Installing Python Packages"
echo "------------------------------------------------------------"
#mkvirtualenv mcjsms
cd ../../
virtualenv env --no-site-packages
source env/bin/activate
pip install -r sms/requirements.txt
pip install -e git+https://github.com/rdegges/django-twilio.git#egg=django_twilio
