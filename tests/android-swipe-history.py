"""Emulator UI acceptance: swipe-to-trash and Undo. Run on unsigned-in fixture data."""
import subprocess,time,re,xml.etree.ElementTree as E
A='/home/jake/.cache/hands-android/sdk/platform-tools/adb'
def adb(*args):return subprocess.check_output([A,'-s','emulator-5554',*args],text=True)
def tree():
 adb('shell','uiautomator','dump','/sdcard/gesture.xml');return E.fromstring(adb('exec-out','cat','/sdcard/gesture.xml'))
def bounds(n):return list(map(int,re.findall(r'\d+',n.get('bounds'))))
def find(s,desc=False,prefix=False):
 for n in tree().iter('node'):
  v=n.get('content-desc' if desc else 'text','')
  if (v.startswith(s) if prefix else v.casefold()==s.casefold()):return bounds(n)
 raise AssertionError(s)
def tap(s,desc=False,prefix=False):
 x,y,r,b=find(s,desc,prefix);adb('shell','input','tap',str((x+r)//2),str((y+b)//2));time.sleep(.4)
def total():
 adb('shell','input','swipe','800','600','800','1800','300');time.sleep(.2)
 result=0
 for n in tree().iter('node'):
  children=list(n)
  if any(c.get('text') in ['YOUR WINS','HER WINS','TIES'] for c in children):
   result+=sum(int(c.get('text')) for c in children if c.get('text','').isdigit())
 return result
def control(name):
 adb('shell','input','swipe','800','1800','800','700','350');time.sleep(.4)
 return find(name+'. Swipe',True,True)
def gesture(name,dy):
 x,y,r,b=control(name);cx=(x+r)//2;cy=(y+b)//2
 adb('shell','input','swipe',str(cx),str(cy),str(cx),str(cy+dy),'200');time.sleep(.5)
# Refuse connected accounts; fixture swipes must never upload to a real scorecard.
prefs=subprocess.run([A,'-s','emulator-5554','shell','run-as','com.jacobsides.hands','cat','shared_prefs/hands.xml'],capture_output=True,text=True)
assert prefs.returncode==0 or 'No such file' in prefs.stderr, 'Use a debuggable emulator fixture app'
assert 'name="token"' not in prefs.stdout, 'Disconnect the fixture account before testing'
tap('History tab',True)
def count():
 for n in tree().iter('node'):
  m=re.fullmatch(r'(\d+) GAMES',n.get('text',''))
  if m:return int(m[1])
 raise AssertionError('count missing')
def swipe(dx,dy=0):
 nodes=[n for n in tree().iter('node') if n.get('content-desc','').startswith('Edit game:')]
 if not nodes:
  adb('shell','input','swipe','800','1800','800','1000','300');nodes=[n for n in tree().iter('node') if n.get('content-desc','').startswith('Edit game:')]
 x,y,r,b=bounds(nodes[0]);cy=(y+b)//2;cx=800 if dx<0 else 250
 adb('shell','input','swipe',str(cx),str(cy),str(cx+dx),str(cy+dy),'300');time.sleep(.4)
base=count();assert base>0
for dx in [-450,450]:
 swipe(dx);assert count()==base-1,'swipe deletes one'
 assert any(n.get('text')=='Game moved to trash' for n in tree().iter('node'))
 tap('Undo');assert count()==base,'undo restores game'
swipe(60);assert count()==base,'short swipe ignored'
swipe(15,-100)
adb('shell','input','swipe','800','700','800','1800','300');assert count()==base,'scroll ignored'
tap('Bulk edit');swipe(-450);assert count()==base,'bulk selection blocks swipe delete'
print('PASS: left/right swipe each trashes one game; visible Undo restores it; short and vertical gestures and bulk selection do not delete.')
