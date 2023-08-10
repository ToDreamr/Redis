local key=KEYS[1];--锁的key
local threadId=ARGV[1];--线程唯一标识
local releaseTime=ARGV[2];--锁的自动释放时间

if(redis.call('exist',key)==0) then
  redis.call('hset',key,threadId,'1');
  redis.call('expire',key,releaseTime);
  return 1;
end;

if(redis.call('exist',key)==1) then
  redis.call('hincrby',key,threadId,'1');
  redis.call('expire',key,releaseTime);
  return 1;
end

return 0;