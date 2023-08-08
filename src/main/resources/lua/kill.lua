--获取参数
local key=KEYS[1]

--当前线程标识
local threadId=ARGV[1]
--获取key
local id=redis.call('get',key)

if(id==threadId) then redis.call('del',key) end

return 0