Project Description — Staff Engagement POC
The problem we're modelling
Across the company, lots of people hold interactions with staff — check-ins, mentoring chats, catch-ups — and capture them in personal notes scattered across individuals, shared only loosely. There's no single place to record an interaction, turn it into follow-up actions, or get a rounded view of a person. We're modelling a small domain that explores solving that.
The domain — core entities and how they relate
Employee — the central record. Everything hangs off a person.
Interaction — a note/record of an engagement with an employee. Many people can log interactions against the same person (e.g. you record notes after chatting to someone). → Many interactions belong to one employee.
Task — interactions can spawn follow-up tasks. Tasks are centralised at the person level: when someone logs in, they see the tasks relating to them, regardless of who created them.
Portfolio — per employee: skills, education history, projects worked on, and public links (GitHub or anything they want to showcase).
Skills register (the centrepiece) — quantifies experience per skill: years and project count on Angular, Java, etc. Should answer questions like "Who's strong on Angular?" with names, years, and number of projects.
You're free to extend the domain as you explore — these are the anchors, not the ceiling.
Two things to keep front of mind
BIG focus on testing
