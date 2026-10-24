"""Acceptance checks for the emulator only. Adds one fixture game, edits it, trashes it, and restores it."""
import os,re,subprocess,time,xml.etree.ElementTree as ET
ADB=os.environ.get('HANDS_ADB','/home/jake/.cache/hands-android/sdk/platform-tools/adb')
SERIAL=os.environ.get('HANDS_EMULATOR','emulator-5554')
assert SERIAL.startswith('emulator-'),'This smoke test may only run on an emulator.'
def adb(*args):return subprocess.check_output([ADB,'-s',SERIAL,*args],text=True,timeout=20)
def dump():
 adb('shell','uiautomator','dump','/sdcard/hands-smoke.xml')
 return ET.fromstring(adb('exec-out','cat','/sdcard/hands-smoke.xml'))
def click(match,desc=False,prefix=False):
 for attempt in range(4):
  tree=dump()
  for node in tree.iter('node'):
   value=node.get('content-desc' if desc else 'text','')
   if (value.startswith(match) if prefix else value.casefold()==match.casefold()):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds','')))
    if y2>y1:
     adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.3);return
  time.sleep(.4)
 raise AssertionError('Could not find '+match)
def texts():return [n.get('text','') for n in dump().iter('node')]
def screenshot(name):
 with open('/tmp/history-of-hands-v2/'+name+'.png','wb') as out:subprocess.run([ADB,'-s',SERIAL,'exec-out','screencap','-p'],stdout=out,check=True)
def count(label):
 for value in texts():
  match=re.fullmatch(r'(\d+) '+label,value)
  if match:return int(match.group(1))
 raise AssertionError('No count for '+label)
def score(label):
 for parent in dump().iter('node'):
  children=list(parent)
  if any(n.get('text')==label for n in children):
   for n in children:
    if n.get('text','').isdigit():return int(n.get('text'))
 raise AssertionError(label)
def search_date(value):
 node=next(n for n in dump().iter('node') if n.get('class')=='android.widget.EditText')
 x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')));adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));adb('shell','input','keycombination','113','29');adb('shell','input','text',value);adb('shell','input','keyevent','KEYCODE_BACK')
adb('shell','am','force-stop','com.jacobsides.hands')
adb('shell','am','start','-n','com.jacobsides.hands/.MainActivity');time.sleep(.8)
wins,losses=score('YOUR WINS'),score('HER WINS')
click('History tab',True);baseline=count('GAMES');click('Play tab',True)
click('+   Log a game');click('You threw rock',True);click('She threw rock',True)
click('History tab',True);assert count('GAMES')==baseline+1;click('Edit game:',True,True)
click('Your throw: Paper',True);click('Her throw: Rock',True);click('Change game date and time',True)
from datetime import date,timedelta
target=date.today()-timedelta(days=1)
chosen=next((n.get('content-desc') for n in dump().iter('node') if re.search(r'\b'+str(target.day)+r'\b',n.get('content-desc','')) and str(target.year) in n.get('content-desc','')),None)
assert chosen,'Previous-day calendar cell not found'
click(chosen,True);click('OK');click('OK');click('Save changes');assert count('GAMES')==baseline+1
expected=str(int((wins+1)*100/(wins+losses+1)+.5))+'%'
click('Insights tab',True);assert expected in texts();screenshot('insights')
click('History tab',True);search_date(target.isoformat());matching=count('GAMES');assert matching>=1
click('Trash');trash=count('DELETED GAMES');click('All');click('Edit game:',True,True);screenshot('editor');click('Move to trash');click('Move to trash')
assert count('GAMES')==matching-1;click('Trash');assert count('DELETED GAMES')==trash+1;click('Restore game:',True,True);assert count('DELETED GAMES')==trash
click('All');assert count('GAMES')==matching;screenshot('history');click('Insights tab',True);assert expected in texts()
print('Android UI passed: two-tap logging, dated history, throw/date correction, analytics recalculation, trash and restore.')
