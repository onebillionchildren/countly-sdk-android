package ly.count.android.sdk.internal;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import java.net.MalformedURLException;
import java.util.List;

import ly.count.android.sdk.Config;
import ly.count.android.sdk.Session;

import static android.support.test.InstrumentationRegistry.getContext;

@RunWith(AndroidJUnit4.class)
public class SessionImplTests {
    @Before
    public void setupEveryTest(){
        android.content.Context context = getContext();
    }

    @After
    public void cleanupEveryTests(){
        //validateMockitoUsage();
    }

    @Test
    public void constructor_empty(){
        final long allowance = 1000000000;
        long time = System.nanoTime();
        SessionImpl session = new SessionImpl();

        long diff = session.getId() - time;
        Assert.assertEquals(true, diff < allowance);
    }

    @Test
    public void constructor_deserialize(){
        long targetID = 11234L;
        SessionImpl session = new SessionImpl(targetID);
        Assert.assertEquals(targetID, (long)session.getId());
    }

    @Test
    public void addParams() throws Exception{
        SessionImpl session = new SessionImpl();
        Assert.assertNull(session.params);

        StringBuilder sbParams = new StringBuilder();
        String[] keys = new String[]{"a", "b", "c"};
        String[] vals = new String[]{"11", "22", "33"};

        sbParams.append(keys[0]).append("=").append(vals[0]);
        Whitebox.<Session>invokeMethod(session, "addParam", keys[0], vals[0]);
        Assert.assertEquals(sbParams.toString(), session.params.toString());

        sbParams.append("&").append(keys[1]).append("=").append(vals[1]);
        Whitebox.<Session>invokeMethod(session, "addParam", keys[1], vals[1]);
        Assert.assertEquals(sbParams.toString(), session.params.toString());

        sbParams.append("&").append(keys[2]).append("=").append(vals[2]);
        Whitebox.<Session>invokeMethod(session, "addParam", keys[2], vals[2]);
        Assert.assertEquals(sbParams.toString(), session.params.toString());
    }

    @Test
    public void beginEndSession_doubleBegin() throws MalformedURLException{
        TestingUtilityInternal.setupLogs();

        long sessionID = 12345;
        SessionImpl session = new SessionImpl(sessionID);

        long beginTime = 234;
        session.begin(beginTime);

        Session tmpSession = session.begin(345L);
        Assert.assertSame(session, tmpSession);
        Assert.assertEquals(sessionID, (long)tmpSession.getId());
        Assert.assertEquals(beginTime, (long)tmpSession.getBegan());

        tmpSession = session.end();
        Assert.assertSame(session, tmpSession);
        Assert.assertEquals(sessionID, (long)tmpSession.getId());
        Assert.assertEquals(beginTime, (long)tmpSession.getBegan());

        tmpSession = session.begin();
        Assert.assertSame(session, tmpSession);
        Assert.assertEquals(sessionID, (long)tmpSession.getId());
        Assert.assertEquals(beginTime, (long)tmpSession.getBegan());
    }

    @Test
    public void beginEndSession_valuesCorrect() {
        SessionImpl session = new SessionImpl();

        session.begin();
        session.end();

        Assert.assertEquals(session.began, session.getBegan());
        Assert.assertEquals(session.ended, session.getEnded());
    }

    @Test
    public void beginEndSession_simple(){
        SessionImpl session = new SessionImpl();
        final long timeGuardStart = System.nanoTime();

        session.begin();
        session.end();

        final long timeGuardEnd = System.nanoTime();
        final long timeGuardDiff = timeGuardEnd - timeGuardStart;

        final long sessionTimeStart = session.getBegan();
        final long sessionTimeEnd = session.getEnded();
        final long sessionTimeDiff = sessionTimeEnd - sessionTimeStart;

        Assert.assertEquals(true, sessionTimeDiff <= timeGuardDiff);
    }

    @Test
    public void secondIsCorrect() {
        long secondInMs = 1000 * 1000 * 1000;
        Assert.assertEquals((double)secondInMs, SessionImpl.SECOND);
    }

    @Test
    public void getBeganAndEnded() {
        long timeBegin = 123L;
        long timeEnd = 345L;
        SessionImpl session = new SessionImpl();
        session.begin(timeBegin);
        session.end(timeEnd);

        Assert.assertEquals(timeBegin, (long)session.getBegan());
        Assert.assertEquals(timeEnd, (long)session.getEnded());
    }

    @Test
    public void beginAfterEnd() throws MalformedURLException {
        TestingUtilityInternal.setupLogs();

        long timeBegin = 123L;
        long timeEnd = 345L;
        SessionImpl session = new SessionImpl();

        session.end();
        Assert.assertEquals(null, session.getBegan());
        Assert.assertEquals(null, session.getEnded());

        session.begin(timeBegin);
        session.end(timeEnd);

        Assert.assertEquals(timeBegin, (long)session.getBegan());
        Assert.assertEquals(timeEnd, (long)session.getEnded());
    }

    @Test
    public void beginAfterEndSet() throws MalformedURLException {
        TestingUtilityInternal.setupLogs();

        long timeEnd = 345L;
        SessionImpl session = new SessionImpl();

        session.ended = timeEnd;
        session.begin();

        Assert.assertEquals(null, session.getBegan());
        Assert.assertEquals(timeEnd, (long) session.getEnded());
    }

    @Test
    public void endedTwice() throws MalformedURLException {
        TestingUtilityInternal.setupLogs();
        long timeBegin = 123L;
        long timeEnd = 345L;
        long timeEndSecond = 678L;
        SessionImpl session = new SessionImpl();

        Assert.assertEquals(null, session.getBegan());
        Assert.assertEquals(null, session.getEnded());

        session.begin(timeBegin);

        Assert.assertEquals(timeBegin, (long) session.getBegan());
        Assert.assertEquals(null, session.getEnded());

        session.end(timeEnd);

        Assert.assertEquals(timeBegin, (long)session.getBegan());
        Assert.assertEquals(timeEnd, (long)session.getEnded());

        session.end(timeEndSecond);

        Assert.assertEquals(timeBegin, (long)session.getBegan());
        Assert.assertEquals(timeEnd, (long)session.getEnded());
    }

    @Test
    public void updateDuration_simple() throws Exception{
        int beginOffsetSeconds = 4;
        long timeBeginOffset = (long) (SessionImpl.SECOND * beginOffsetSeconds);

        SessionImpl session = new SessionImpl();
        final long timeGuardBase = System.nanoTime() - timeBeginOffset;
        session.begin(timeGuardBase);

        final long timeGuardStart = System.nanoTime();
        Long duration = Whitebox.<Long>invokeMethod(session, "updateDuration");
        final long timeGuardEnd = System.nanoTime();

        final long timeGuardDurStart = Math.round((timeGuardStart - timeGuardBase) / SessionImpl.SECOND);
        final long timeGuardDurEnded = Math.round((timeGuardEnd - timeGuardBase) / SessionImpl.SECOND);

        Assert.assertEquals(true, duration >= timeGuardDurStart);
        Assert.assertEquals(true, duration <= timeGuardDurEnded);
    }

    @Test
    public void updateDuration_notFirstUpdate() throws Exception{
        int beginOffsetSeconds = 12;
        long timeBeginOffset = (long) (SessionImpl.SECOND * beginOffsetSeconds);

        final long timeGuardBase = System.nanoTime() - timeBeginOffset;
        SessionImpl session = new SessionImpl();
        session.begin();
        Whitebox.setInternalState(session, "updated", timeGuardBase);

        final long timeGuardStart = System.nanoTime();
        Long duration = Whitebox.<Long>invokeMethod(session, "updateDuration");
        final long timeGuardEnd = System.nanoTime();

        final long timeGuardDurStart = Math.round((timeGuardStart - timeGuardBase) / SessionImpl.SECOND);
        final long timeGuardDurEnded = Math.round((timeGuardEnd - timeGuardBase) / SessionImpl.SECOND);

        Assert.assertEquals(true, duration >= timeGuardDurStart);
        Assert.assertEquals(true, duration <= timeGuardDurEnded);
    }

    @Test
    public void beginEndSession_simpleWithWait() throws InterruptedException{
        long sleepTimeMs = 500;

        SessionImpl session = new SessionImpl();
        final long timeGuardStart = System.nanoTime();

        session.begin();
        Thread.sleep(sleepTimeMs);
        session.end();

        final long timeGuardEnd = System.nanoTime();
        final long timeGuardDiff = timeGuardEnd - timeGuardStart;

        final long sessionTimeStart = session.getBegan();
        final long sessionTimeEnd = session.getEnded();
        final long sessionTimeDiff = sessionTimeEnd - sessionTimeStart;

        Assert.assertEquals(true, sessionTimeDiff >= (sleepTimeMs * 1000));
        Assert.assertEquals(true, sessionTimeDiff <= timeGuardDiff);
    }


    @Test
    public void update_simple() throws MalformedURLException {
        TestingUtilityInternal.setupLogs();
        long timeBegin = 123L;
        long timeEnd = 345L;
        SessionImpl session = new SessionImpl();

        session.update();

        Assert.assertEquals(null, session.getBegan());
        Assert.assertEquals(null, session.getEnded());

        session.begin(timeBegin);
        session.update();

        Assert.assertEquals(timeBegin, (long) session.getBegan());
        Assert.assertEquals(null, session.getEnded());

        session.end(timeEnd);
        session.update();

        Assert.assertEquals(timeBegin, (long)session.getBegan());
        Assert.assertEquals(timeEnd, (long)session.getEnded());
    }

    @Test
    public void isLeading_null() throws MalformedURLException{
        Core core = TestingUtilityInternal.setupBasicCore();
        SessionImpl session = new SessionImpl();
        Assert.assertEquals(false, (boolean)session.isLeading());
    }
    @Test
    public void isLeading_succeed() throws MalformedURLException{
        Core core = TestingUtilityInternal.setupBasicCore();
        List<SessionImpl> sessions = Whitebox.<List<SessionImpl>>getInternalState(core, "sessions");
        SessionImpl sessionTarget = new SessionImpl(123L);
        sessions.add(sessionTarget);
        Assert.assertEquals(true, (boolean)sessionTarget.isLeading());
    }
    @Test
    public void isLeading_fail() throws MalformedURLException{
        Core core = TestingUtilityInternal.setupBasicCore();
        List<SessionImpl> sessions = Whitebox.<List<SessionImpl>>getInternalState(core, "sessions");

        SessionImpl sessionOther = new SessionImpl(456L);
        SessionImpl sessionTarget = new SessionImpl(123L);
        sessions.add(sessionOther);
        Assert.assertEquals(false, (boolean)sessionTarget.isLeading());
    }
}